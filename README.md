# SpotFinder for OSU

SpotFinder is a dedicated parking app developed for Oregon State University. It helps users to find most suitable parking lot based on real-time data and their estimated arrival time. 

## Key Problems Addressed

- **Real-time parking spot availability for OSU parking lots.**
- **Search-and-Filter Problem: Find all available parking lots and filter the most suitable one for the user.**
- **Coordination Problem: Ensure two drivers don't aim for the same spot at the same time**

## Features

- 🚗 **Real-Time Parking Spot Availability**: Gain insights into live parking spot availability across all campus parking lots
  
- 🔍 **Smart Recommendations**: SpotFinder's algorithms will suggest the best parking lot based on current spot availability and your predicted time of arrival.
  
- 🅿️ **Soft Reservations**: A unique feature that offers a temporary hold on a parking spot. It's not a confirmed reservation, but it helps in channeling user traffic, ensuring multiple users aren't directed to the same spot.
  
## Requirements

- Google Maps Platform account (for API key).
- Android Studio.

## Setup Steps

1. **Clone the SpotFinder Repository**:
   ```
   git clone https://github.com/imshrey23/parking-lot-application.git
   ```
2. **Add the Google platform API key in .env file** 
3. **Set up the user api server from the git repository**:
   ```
   git clone https://github.com/imshrey23/user-api.git
   ```
4. **Set up the Smart Park API. Follow the steps given**:
   ```
   git clone https://github.com/subramanya1702/Smart-Park
   ```
5. **Open the cloned project in Android Studio. Run the Android application.**

## How it works

1. Launch SpotFinder
2. Give required location permission
3. The most suitable parking lot based on the current location is displayed
4. To get parking lots based on destination location, search the destination location in the search bar
5. The most suitable parking lot based on the destination location is displayed
6. To get more information about the parking lot, click more information button on the right side
7. To get turn-by-turn navigation to parking lot from current location, click directions button

## Feedback & Support

For any queries, issues, or suggestions, please reach out to dhumes@oregonstate.edu

   
