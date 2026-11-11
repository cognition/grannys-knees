# How to use Granny's Knees

## 1. Choose a forecast location

On first launch, search for a Canadian city and select the matching result. The selected city is saved on the phone and can be changed later.

You can also choose **Use this phone's location**. This is optional. The app requests foreground coarse or fine location only for choosing a nearby place; manual city search continues to work if permission is declined.

## 2. Read Today

The Today screen shows:

- the selected city and its local time;
- the current weather-index value and a short band such as Mild or Rough;
- when the forecast was fetched;
- the scored hours remaining in the local day; and
- the next five local days' low and high index values.

The main display rounds the calculated value to a whole number. The charts and daily ranges retain one decimal place.

An empty part of a curve means the hour could not be scored. It is not a zero. If the forecast is missing, too old, incomplete, or unavailable offline, the app explains that state instead of inventing a value.

## 3. Change the city or refresh

Open the place controls to search for another city. Choosing a new city stores its coordinates, Canadian region, country code, and IANA timezone, then fetches a forecast for that place.

Use refresh when you want an immediate network attempt. Automatic fetching is also controlled by the saved city's local calendar day so the app does not repeatedly request the same forecast every time the screen opens.

## 4. Record how you feel

Open **How you feel** and select a body part. You may choose a side, select **ache** or **stiffness**, and set a rating.

- A rating is an integer from 0 to 10. For ache, the slider starts at 1; zero is still a valid stored rating when entered through the supported journal flow.
- Only the places you report are saved. Leaving a place untouched does not record a zero.
- **It is unchanged** records an explicit unchanged confirmation.
- Each save records the time it was written.

The journal is stored in the phone's local Room database. The weather index and your symptom ratings are not combined into one number.

## 5. Review, export, or delete journal data

Use **History** to review saved reports. Individual reports can be removed. From Settings, **Export history** opens the Android share sheet with the current journal as JSON; sharing is user-initiated and the destination is chosen by the user.

**Reset all data** clears the local journal, today's draft figure, and local Feel calibration. Uninstalling the app or clearing its Android storage also removes app-private data.

## 6. Tune Personal, if desired

Settings can enable the Personal series. The sliders describe how strongly pressure, humidity, and temperature influence you; they do not mean high versus low weather.

Feel calibration remains off until there are ten usable, separated check-ins. Saves within 15 minutes count as one time-point, and counted time-points must be at least 45 minutes apart. Calibration is local to the phone and changes only Personal. The baseline Weather score remains forecast-based.

## 7. Understand the limitations

The index is experimental and uses forecast weather as a heuristic signal. Correlation is not causation. It does not diagnose arthritis, predict a flare, or replace medical advice.
