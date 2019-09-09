package in.slanglabs.travel.flights;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import in.slanglabs.platform.SlangBuddy;
import in.slanglabs.platform.SlangBuddyOptions;
import in.slanglabs.platform.SlangIntent;
import in.slanglabs.platform.SlangLocale;
import in.slanglabs.platform.SlangSession;
import in.slanglabs.platform.action.SlangIntentAction;
import in.slanglabs.platform.action.SlangUtteranceAction;
import in.slanglabs.platform.prompt.SlangMessage;

import static in.slanglabs.travel.flights.AppTravelAction.processGatherMissingData;
import static in.slanglabs.travel.flights.AppTravelAction.processInvalidUtterance;
import static in.slanglabs.travel.flights.AppTravelAction.processSearchFlights;
import static in.slanglabs.travel.flights.AppTravelAction.processSortFlights;
import static in.slanglabs.travel.flights.AppTravelAction.resetCache;

class SlangInterface {

    private static final String BUDDY_ID = "6b7a80d87e9a491ea7316b1ee051f4f0";
    private static final String API_KEY = "9676113588104021af6b2c7641845b03";

    private static SlangIntentAction sAppActionHandler;
    // To initialize Slang in your application, simply call SlangInterface.init(context)
    static void init(Activity activity) {
        try {
            sAppActionHandler = new TravelAction();
            SlangBuddyOptions options = new SlangBuddyOptions.Builder()
                    .setApplication(activity.getApplication())
                    .setBuddyId(BUDDY_ID)
                    .setAPIKey(API_KEY)
                    .setListener(new BuddyListener(activity.getApplicationContext()))
                    .setIntentAction(sAppActionHandler)
                    .setUtteranceAction(new UtteranceHandler())
                    .setRequestedLocales(new HashSet<Locale>() {{
                        add(SlangLocale.LOCALE_ENGLISH_US);
                    }})
                    .setDefaultLocale(SlangLocale.LOCALE_ENGLISH_US)
                    // change env to production when the buddy is published to production
                    .setEnvironment(SlangBuddy.Environment.STAGING)
                    .setConfigOverrides(getConfigOverrides())
                    .setStartActivity(activity)
                    .build();
            SlangBuddy.initialize(options);

        } catch (SlangBuddyOptions.InvalidOptionException e) {
            e.printStackTrace();
        } catch (SlangBuddy.InsufficientPrivilegeException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Object> getConfigOverrides() {
        HashMap<String, Object> config = new HashMap<>();

        //config.put("internal.subsystems.asr.force_cloud_asr", true);

        return config;
    }

    static void startConversation(String msg, boolean isSpoken) {
        try {
            HashMap<Locale, String> strings = new HashMap<>();
            strings.put(SlangLocale.LOCALE_ENGLISH_US, msg);
            SlangMessage message = SlangMessage.create(strings);
            SlangBuddy.startConversation(message, isSpoken);
        } catch (SlangBuddy.UninitializedUsageException e) {
            e.printStackTrace();
        }
    }

    private static class BuddyListener implements SlangBuddy.Listener {
        private Context appContext;

        BuddyListener(Context appContext) {
            this.appContext = appContext;
        }

        @Override
        public void onInitialized() {
            Log.d("BuddyListener", "Slang Initialised Successfully");
            try {
                SlangBuddy.registerIntentAction("slang_help", sAppActionHandler);
            } catch (SlangBuddy.InvalidIntentException e) {
                e.printStackTrace();
            } catch (SlangBuddy.UninitializedUsageException e) {
                e.printStackTrace();
            }

            SlangBuddy.getBuiltinUI().disableLocaleSelection(true);
            //Start with providing hints to do the search.
            SlangBuddy.getBuiltinUI().setIntentFiltersForDisplay(
                    new HashSet<String>()
                    {{
                        add(TravelAction.INTENT_SEARCH_FLIGHTS);
                    }}
            );
            AppTravelAction.processSlangInitialised(appContext);
        }

        @Override
        public void onInitializationFailed(final SlangBuddy.InitializationError e) {
            Log.d("BuddyListener", "Slang failed:" + e.getMessage());

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(appContext, "Failed to initialise Slang:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }, 10);
        }

        @Override
        public void onLocaleChanged(final Locale newLocale) {}

        @Override
        public void onLocaleChangeFailed(final Locale newLocale, final SlangBuddy.LocaleChangeError e) {}
    }

    public static class TravelAction implements SlangIntentAction {

        static final String INTENT_SEARCH_FLIGHTS = "search_flights";
        static final String INTENT_SORT_FLIGHTS = "sort_flights";
        static final String INTENT_GATHER_MISSING_DATA = "gather_missing_data";
        static final String INTENT_HELP = "slang_help";

        TravelAction() {
            resetCache();
        }

        @Override
        public Status action(SlangIntent intent, SlangSession session) {
            switch (intent.getName()) {
                case INTENT_SEARCH_FLIGHTS:
                    processSearchFlights(intent, session);
                    break;
                case INTENT_SORT_FLIGHTS:
                    processSortFlights(intent, session);
                    break;
                case INTENT_GATHER_MISSING_DATA:
                    processGatherMissingData(intent, session);
                    break;
                case INTENT_HELP:
                    intent.getCompletionStatement().overrideAffirmative("Please try one of the options mentioned on the screen");
                    break;
            }

            return Status.SUCCESS;
        }
    }

    private static class UtteranceHandler implements SlangUtteranceAction {
        @Override
        public void onUtteranceDetected(String s, SlangSession slangSession) {
            //NOP
        }

        @Override
        public Status onUtteranceUnresolved(String s, SlangSession slangSession) {
            final Pair<String, Boolean> result = processInvalidUtterance(s, slangSession);
            if (null != result && null != result.first && !result.first.isEmpty()) {
                try {
                    HashMap<Locale, String> strings = new HashMap<>();
                    strings.put(SlangLocale.LOCALE_ENGLISH_US, result.first);
                    SlangBuddy.getClarificationMessage().overrideMessages(strings);
                    if (null != result.second && result.second) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                SlangInterface.startConversation(result.first, false);
                            }
                        }, 1000);
                    }
                    return Status.SUCCESS;
                } catch (SlangBuddy.UninitializedUsageException e) {
                    e.printStackTrace();
                }
            }

            return Status.FAILURE;
        }
    }
}
