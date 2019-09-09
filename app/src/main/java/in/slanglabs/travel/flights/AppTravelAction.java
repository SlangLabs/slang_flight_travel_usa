package in.slanglabs.travel.flights;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import org.json.JSONObject;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.slanglabs.platform.SlangBuddy;
import in.slanglabs.platform.SlangEntity;
import in.slanglabs.platform.SlangIntent;
import in.slanglabs.platform.SlangLocale;
import in.slanglabs.platform.SlangSession;

import static in.slanglabs.travel.flights.SlangInterface.TravelAction.INTENT_SEARCH_FLIGHTS;
import static in.slanglabs.travel.flights.SlangInterface.TravelAction.INTENT_SORT_FLIGHTS;

class AppTravelAction {
    private static Map<String, String> sEntities2PromptMap = new HashMap<>();
    private static Map<String, String> sEntities2ValuesMap = new HashMap<>();
    private static Map<String, String> cityToAirportMap, airportToCityMap;

    private static String prevIntentName = "";
    private static String nextExpectedEntityName = "";
    private static boolean isMinDataToSearchAvailabe;

    private static final String ENTITY_SOURCE = "src_city";
    private static final String ENTITY_DESTINATION = "dest_city";
    private static final String ENTITY_START_DATE = "start_date";
    private static final String ENTITY_TIME_OF_THE_DAY = "time_of_the_day";
    private static final String ENTITY_RANGE = "range";
    private static final String ENTITY_TIME_ONE = "time_one";
    private static final String ENTITY_TIME_TWO = "time_two";
    private static final String ENTITY_NEGATE = "negate";
    private static final String ENTITY_DEPARTING = "departing";
    private static final String ENTITY_ARRIVING = "arriving";
    private static final String ENTITY_TRAVEL_CLASS = "travel_class";
    private static final String ENTITY_CITY_NAME = "city_name";
    private static final String ENTITY_DATE = "date";
    private static final String ENTITY_SORT_TYPE = "sort_type";

    private static final String MESSAGE_NOT_SEARCHED_EN = "Hey, I'm miserably failing to read your mind, did you miss searching for the flights first?";

    static void processSlangInitialised(Context context) {
        cityToAirportMap = loadMap(context, R.raw.city2airport);
        airportToCityMap = loadMap(context, R.raw.airport2city);
    }

    private static Map<String, String> loadMap(Context context, int resourceId) {
        Map<String, String> map = new HashMap<>();
        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            JSONObject jsonObject = new JSONObject(new String(buffer, "UTF-8"));
            Iterator<String> keys = jsonObject.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                String value = jsonObject.getString(key);
                map.put(key.toLowerCase(), value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    static void processSearchFlights(SlangIntent intent, SlangSession session) {
        extractSearchDetails(intent);
        Pair<String, Boolean> result = searchFlights(session);

        if (null != result && null != result.first) {
            intent.getCompletionStatement().overrideAffirmative(result.first);
            if (null != result.second && result.second) {
                SlangInterface.startConversation(result.first, false);
            }
        }

        prevIntentName = intent.getName();
    }

    private static Pair<String, Boolean> searchFlights(SlangSession session) {
        String promptToSpeak = null;

        //Fill up the form with valid spoken data.
        if (session.getCurrentActivity() instanceof MainActivity) {
            MainActivity activity = ((MainActivity) session.getCurrentActivity());
            if(!sEntities2ValuesMap.get(ENTITY_SOURCE).isEmpty()) {
                String nameToDisplay = sEntities2ValuesMap.get(ENTITY_SOURCE);
                if (airportToCityMap.containsKey(nameToDisplay.toLowerCase())) {
                    nameToDisplay = airportToCityMap.get(nameToDisplay.toLowerCase());
                }
                activity.setSource(nameToDisplay);
            }
            if (!sEntities2ValuesMap.get(ENTITY_DESTINATION).isEmpty()) {
                String nameToDisplay = sEntities2ValuesMap.get(ENTITY_DESTINATION);
                if (airportToCityMap.containsKey(nameToDisplay.toLowerCase())) {
                    nameToDisplay = airportToCityMap.get(nameToDisplay.toLowerCase());
                }
                activity.setDestination(nameToDisplay);
            }
            if (!sEntities2ValuesMap.get(ENTITY_START_DATE).isEmpty()) {
                activity.setStartDate(sEntities2ValuesMap.get(ENTITY_START_DATE));
            }
        }

        //Setup assistance/hint text to help user speak out the missing data.
        List<String> engHelpStringsToDisplay = new ArrayList<>();
        List<String> hiHelpStringsToDisplay = new ArrayList<>();

        if (sEntities2ValuesMap.get(ENTITY_SOURCE).isEmpty()) {
            promptToSpeak = getMissingEntityPrompt(ENTITY_SOURCE);
            engHelpStringsToDisplay = Arrays.asList(engMissingSrcCityHints);
            nextExpectedEntityName = ENTITY_SOURCE;
        } else if (sEntities2ValuesMap.get(ENTITY_DESTINATION).isEmpty()) {
            promptToSpeak = getMissingEntityPrompt(ENTITY_DESTINATION);
            engHelpStringsToDisplay = Arrays.asList(engMissingDestCityHints);
            nextExpectedEntityName = ENTITY_DESTINATION;
        } else if (sEntities2ValuesMap.get(ENTITY_START_DATE).isEmpty()) {
            promptToSpeak = getMissingEntityPrompt(ENTITY_START_DATE);
            engHelpStringsToDisplay = Arrays.asList(engMissingStartDateHints);
            nextExpectedEntityName = ENTITY_START_DATE;
        }

        //If there is any mandatory parameters missing, assist user in collecting them.
        //If nothing missing, navigate to the result page.
        if (promptToSpeak != null && !promptToSpeak.isEmpty()) {
            if (!engHelpStringsToDisplay.isEmpty()) {
                Map<Locale, List<String>> customHelpStrings = new HashMap<>();
                customHelpStrings.put(SlangLocale.LOCALE_ENGLISH_IN, engHelpStringsToDisplay);
                customHelpStrings.put(SlangLocale.LOCALE_HINDI_IN, hiHelpStringsToDisplay);
                SlangBuddy.getBuiltinUI().setAssistanceText(customHelpStrings);
            }
            return new Pair<>(promptToSpeak, true);
        } else {
            isMinDataToSearchAvailabe = true;
            if (session.getCurrentActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) session.getCurrentActivity();
                Intent intent = new Intent(mainActivity, DetailsActivity.class);
                intent.putExtra("search_criteria", getSearchCompletionStatement());
                intent.putExtra("locale", session.getCurrentLocale().getLanguage());
                mainActivity.startActivity(intent);
                return new Pair<>(getSearchCompletionStatement(), false);
            } else if ((session.getCurrentActivity() instanceof DetailsActivity)) {
                // refresh activity
                DetailsActivity detailsActivity = (DetailsActivity) session.getCurrentActivity();
                detailsActivity.setSort(session.getCurrentActivity().getResources().getString(R.string.wow_so_many_flights));
                detailsActivity.setUpdated();
                detailsActivity.updateSearchCriteria(getSearchCompletionStatement());
                return new Pair<>("Showing updated list of flights.", false);
            }
        }

        return null;
    }

    static void processSortFlights(SlangIntent intent, SlangSession session) {
        if (!isMinDataToSearchAvailabe) {
            intent.getCompletionStatement().overrideAffirmative(MESSAGE_NOT_SEARCHED_EN);
            return;
        }

        SlangEntity sort = intent.getEntity(ENTITY_SORT_TYPE);
        Pair<String, Boolean> result = sortFlights(sort.getValue(), sort.getPrompt().getQuestion(), session);
        if (null != result.first) {
            intent.getCompletionStatement().overrideAffirmative(result.first);
            if (null != result.second && result.second) {
                SlangInterface.startConversation(result.first, false);
            }
        }

        prevIntentName = intent.getName();
    }

    private static Pair<String, Boolean> sortFlights(String sortType, String promptToSpeak, SlangSession session) {
        if (null != sortType && !sortType.isEmpty()) {
            if (session.getCurrentActivity() instanceof DetailsActivity) {
                promptToSpeak = "Showing you the flights sorted by " + sortType;
                ((DetailsActivity) session.getCurrentActivity()).setSort(promptToSpeak);
            }
            return new Pair<>(promptToSpeak, false);
        } else {
            nextExpectedEntityName = ENTITY_SORT_TYPE;
            return new Pair<>(promptToSpeak, true);
        }
    }

    static void processGatherMissingData(SlangIntent intent, SlangSession session) {
        if (null == nextExpectedEntityName || nextExpectedEntityName.isEmpty()) {
            intent.getCompletionStatement().overrideAffirmative(
                    "Sorry, I wasn't expecting that, You're smarter than me for sure! Please try one of the examples shown on screen.");
            return;
        }

        SlangEntity cityNameEntity = intent.getEntity(ENTITY_CITY_NAME);
        SlangEntity dateEntity = intent.getEntity(ENTITY_DATE);
        SlangEntity sortTypeEntity = intent.getEntity(ENTITY_SORT_TYPE);

        Pair<String, Boolean> result = null;
        if (prevIntentName.equals(INTENT_SEARCH_FLIGHTS)) {
            switch (nextExpectedEntityName) {
                case ENTITY_SOURCE:
                case ENTITY_DESTINATION:
                    if(null != cityNameEntity && cityNameEntity.isResolved()) {
                        result = processMissingSearchData(
                                nextExpectedEntityName,
                                cityNameEntity.getValue(),
                                cityNameEntity.getPrompt().getQuestion(),
                                session);
                    } else {
                        result = new Pair<>("That didn't fit the bill, can you please try again?", true);
                    }
                    break;
                case ENTITY_START_DATE:
                    if (null != dateEntity && dateEntity.isResolved()) {
                        result = processMissingSearchData(
                                ENTITY_START_DATE,
                                dateEntity.getValue(),
                                dateEntity.getPrompt().getQuestion(),
                                session);
                    } else {
                        result = new Pair<>("That didn't fit the bill, can you please try again?", true);
                    }
                    break;
            }
        } else if (prevIntentName.equals(INTENT_SORT_FLIGHTS)) {
            if (nextExpectedEntityName.equals(ENTITY_SORT_TYPE)) {
                String sortTypePrompt = "Sorry, we missed that. " + sortTypeEntity.getPrompt().getQuestion();
                result = sortFlights(sortTypeEntity.getValue(), sortTypePrompt, session);
            }
        }

        if (null != result) {
            if (null != result && null != result.first) {
                intent.getCompletionStatement().overrideAffirmative(result.first);
                if (null != result.second && result.second) {
                    SlangInterface.startConversation(result.first, false);
                }
            }
        }
    }

    static Pair<String, Boolean> processInvalidUtterance(String utterance, SlangSession session) {
        Pair<String, Boolean> result = null;
        if (prevIntentName.equals(INTENT_SEARCH_FLIGHTS)) {
            switch (nextExpectedEntityName) {
                case ENTITY_SOURCE:
                case ENTITY_DESTINATION:
                    if (cityToAirportMap.containsKey(utterance.toLowerCase().trim())) {
                        result = processMissingSearchData(
                                nextExpectedEntityName,
                                utterance.trim(),
                                "Sorry, we didn't understand it, can you please try again",
                                session);
                    }
                    break;
            }
        } else if (prevIntentName.equals(INTENT_SORT_FLIGHTS)) {
            if (nextExpectedEntityName.equals(ENTITY_SORT_TYPE)) {
                String sortTypePrompt = "Sorry, we missed that. Can you please try again";
                result = sortFlights(utterance, sortTypePrompt, session);
            }
        }

        return result;
    }


    private static final String[] engMissingSrcCityHints = {
            "I am travelling from Palo Alto, CA",
            "Flights from San Francisco",
            "From San jose"
    };

    private static final String[] engMissingDestCityHints = {
            "Going to Mumbai",
            "To Chennai",
            "Delhi"
    };

    private static final String[] engMissingStartDateHints = {
            "Tomorrow",
            "Day after tomorrow",
            "24th August"
    };

    private static String getMissingEntityPrompt(String entityName) {
        switch (entityName) {
            case ENTITY_SOURCE:
                if (!sEntities2PromptMap.get(ENTITY_SOURCE).isEmpty()) {
                    return sEntities2PromptMap.get(ENTITY_SOURCE);
                } else {
                    return "Where are you travelling from?";
                }
            case ENTITY_DESTINATION:
                if (!sEntities2PromptMap.get(ENTITY_DESTINATION).isEmpty()) {
                    return sEntities2PromptMap.get(ENTITY_DESTINATION);
                } else {
                    return "Where are you travelling to?";
                }
            case ENTITY_START_DATE:
                if (!sEntities2PromptMap.get(ENTITY_START_DATE).isEmpty()) {
                    return sEntities2PromptMap.get(ENTITY_START_DATE);
                } else {
                    return "When are you travelling?";
                }
            default:
                return "";
        }
    }

    private static String getSearchCompletionStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("Showing you the");
        if (!sEntities2ValuesMap.get(ENTITY_TIME_OF_THE_DAY).isEmpty()) {
            sb.append(" ").append(sEntities2ValuesMap.get(ENTITY_TIME_OF_THE_DAY));
        }
        sb.append(" flights");
        if (!sEntities2ValuesMap.get(ENTITY_TRAVEL_CLASS).isEmpty()) {
            sb.append(" with");
            if (!sEntities2ValuesMap.get(ENTITY_NEGATE).isEmpty()) {
                sb.append(" non");
            }
            sb.append(" ").append(sEntities2ValuesMap.get(ENTITY_TRAVEL_CLASS));
        }
        sb.append(" from ")
                .append(sEntities2ValuesMap.get(ENTITY_SOURCE))
                .append(" to ")
                .append(sEntities2ValuesMap.get(ENTITY_DESTINATION))
                .append(" travelling on ")
                .append(sEntities2ValuesMap.get(ENTITY_START_DATE));
        if (!sEntities2ValuesMap.get(ENTITY_RANGE).isEmpty()) {
            StringBuilder sb1 = new StringBuilder();
            switch (sEntities2ValuesMap.get(ENTITY_RANGE)) {
                case "before":
                    if (!sEntities2ValuesMap.get(ENTITY_TIME_TWO).isEmpty()) {
                        sb1.append(" ")
                                .append(getSpeakableTime(sEntities2ValuesMap.get(ENTITY_TIME_TWO)));
                    }
                    break;
                case "after":
                    if (!sEntities2ValuesMap.get(ENTITY_TIME_ONE).isEmpty()) {
                        sb1.append(" ")
                                .append(getSpeakableTime(sEntities2ValuesMap.get(ENTITY_TIME_ONE)));
                    }
                    break;
                case "between":
                    if (!sEntities2ValuesMap.get(ENTITY_TIME_ONE).isEmpty()
                            && !sEntities2ValuesMap.get(ENTITY_TIME_TWO).isEmpty()) {
                        sb1.append(" ")
                                .append(getSpeakableTime(sEntities2ValuesMap.get(ENTITY_TIME_ONE)))
                                .append(" and ")
                                .append(getSpeakableTime(sEntities2ValuesMap.get(ENTITY_TIME_TWO)));
                    }
                    break;
            }
            if (!sb1.toString().isEmpty()) {
                sb.append(" and ");
                if (!sEntities2ValuesMap.get(ENTITY_DEPARTING).isEmpty()) {
                    sb.append(" leaving ");
                } else if (!sEntities2ValuesMap.get(ENTITY_ARRIVING).isEmpty()) {
                    sb.append(" arriving ");
                } else {
                    sb.append(" leaving ");
                }
                sb.append(sEntities2ValuesMap.get(ENTITY_RANGE))
                        .append(sb1.toString());
            }
        }

        return sb.toString();
    }

    private static Pair<String, Boolean> processMissingSearchData(
            String entityName,
            String entityValue,
            String prompt,
            SlangSession session
    ) {
        sEntities2ValuesMap.put(entityName, entityValue);
        sEntities2PromptMap.put(
                entityName,
                "Sorry, we missed that. " + prompt
        );
        return searchFlights(session);
    }

    private static String getSpeakableTime(String time) {
        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
        SimpleDateFormat formatSlang = new SimpleDateFormat("h:mm a", Locale.getDefault());
        try {
            Date date = format.parse(time);
            return String.valueOf(formatSlang.format(date));
        } catch (ParseException e) {
            e.printStackTrace();
            return time;
        }
    }

    public static void setTouchEntities(String src, String dest, String startDate) {
        if (null != src && !src.isEmpty()) {
            sEntities2ValuesMap.put(ENTITY_SOURCE, src);
        }
        if (null != dest && !dest.isEmpty()) {
            sEntities2ValuesMap.put(ENTITY_DESTINATION, dest);
        }
        if (null != startDate && !startDate.isEmpty()) {
            sEntities2ValuesMap.put(ENTITY_START_DATE, startDate);
        }
    }

    private static void extractSearchDetails(SlangIntent intent) {
        SlangEntity entitySource = intent.getEntity(ENTITY_SOURCE);
        sEntities2PromptMap.put(ENTITY_SOURCE, entitySource.getPrompt().getQuestion());

        SlangEntity entityDestination = intent.getEntity(ENTITY_DESTINATION);
        sEntities2PromptMap.put(ENTITY_DESTINATION, entityDestination.getPrompt().getQuestion());

        SlangEntity entityStartDate = intent.getEntity(ENTITY_START_DATE);
        sEntities2PromptMap.put(ENTITY_START_DATE, entityStartDate.getPrompt().getQuestion());

        SlangEntity entityRange = intent.getEntity(ENTITY_RANGE);
        SlangEntity entityTimeOfTheDay = intent.getEntity(ENTITY_TIME_OF_THE_DAY);
        SlangEntity entityTimeOne = intent.getEntity(ENTITY_TIME_ONE);
        SlangEntity entityTimeTwo = intent.getEntity(ENTITY_TIME_TWO);
        SlangEntity entityDepart = intent.getEntity(ENTITY_DEPARTING);
        SlangEntity entityArrive = intent.getEntity(ENTITY_ARRIVING);
        SlangEntity entityNegate = intent.getEntity(ENTITY_NEGATE);
        SlangEntity entityTravelClass = intent.getEntity(ENTITY_TRAVEL_CLASS);

        if (entitySource.isResolved()) {
            String entityValue = entitySource.getValue();
            sEntities2ValuesMap.put(
                    ENTITY_SOURCE,
                    airportToCityMap.containsKey(entityValue.toLowerCase())
                            ? airportToCityMap.get(entityValue.toLowerCase())
                            : entityValue
            );
        }
        if (entityDestination.isResolved()) {
            String entityValue = entityDestination.getValue();
            sEntities2ValuesMap.put(ENTITY_DESTINATION,
                    airportToCityMap.containsKey(entityValue.toLowerCase())
                            ? airportToCityMap.get(entityValue.toLowerCase())
                            : entityValue
            );
        }
        if (entityStartDate.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_START_DATE, entityStartDate.getValue());
        }
        if (null != entityRange && entityRange.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_RANGE, entityRange.getValue());
        }
        if (null != entityTimeOfTheDay && entityTimeOfTheDay.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_TIME_OF_THE_DAY, entityTimeOfTheDay.getValue());
        }
        if (null != entityTimeOne && entityTimeOne.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_TIME_ONE, entityTimeOne.getValue());
        }
        if (null != entityTimeTwo && entityTimeTwo.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_TIME_TWO, entityTimeTwo.getValue());
        }
        if (null != entityNegate && entityNegate.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_NEGATE, entityNegate.getValue());
        }
        if (null != entityTravelClass && entityTravelClass.isResolved()) {
            String travelClass;
            if (entityTravelClass.isList()) {
                StringBuilder travelClassBuilder = new StringBuilder();
                for (SlangEntity tClass: entityTravelClass.getListValues()) {
                    travelClassBuilder.append(
                            travelClassBuilder.toString().isEmpty() ? tClass.getValue() : " and "
                                    + tClass.getValue()
                    );
                }
                travelClass = travelClassBuilder.toString();
            } else {
                travelClass = entityTravelClass.getValue();
            }
            sEntities2ValuesMap.put(ENTITY_TRAVEL_CLASS, travelClass);
        }
        if (null != entityDepart && entityDepart.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_DEPARTING, entityDepart.getValue());
        }
        if (null != entityArrive && entityArrive.isResolved()) {
            sEntities2ValuesMap.put(ENTITY_ARRIVING, entityArrive.getValue());
        }
    }

    static void resetCache() {
        prevIntentName = "";
        nextExpectedEntityName = "";
        isMinDataToSearchAvailabe = false;

        sEntities2PromptMap.put(ENTITY_SOURCE, "");
        sEntities2PromptMap.put(ENTITY_DESTINATION, "");
        sEntities2PromptMap.put(ENTITY_START_DATE, "");

        sEntities2ValuesMap.put(ENTITY_SOURCE, "");
        sEntities2ValuesMap.put(ENTITY_DESTINATION, "");
        sEntities2ValuesMap.put(ENTITY_START_DATE, "");
        sEntities2ValuesMap.put(ENTITY_RANGE, "");
        sEntities2ValuesMap.put(ENTITY_TIME_OF_THE_DAY, "");
        sEntities2ValuesMap.put(ENTITY_TIME_ONE, "");
        sEntities2ValuesMap.put(ENTITY_TIME_TWO, "");
        sEntities2ValuesMap.put(ENTITY_TRAVEL_CLASS, "");
        sEntities2ValuesMap.put(ENTITY_NEGATE, "");
        sEntities2ValuesMap.put(ENTITY_ARRIVING, "");
        sEntities2ValuesMap.put(ENTITY_DEPARTING, "");
    }
}
