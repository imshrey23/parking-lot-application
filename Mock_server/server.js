require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const app = express();
const port = process.env.PORT || 8000;
const mongoUsername = process.env.MONGO_USERNAME;
const mongoPassword = process.env.MONGO_PASSWORD;

// Connect to MongoDB Atlas
mongoose.connect(`mongodb+srv://${mongoUsername}:${mongoPassword}@msproject.jfkgya5.mongodb.net/test?retryWrites=true&w=majority`, {
    useNewUrlParser: true,
    useUnifiedTopology: true
});

// Define a schema and model for the data
const DataSchema = new mongoose.Schema({
  parkingLotName: String,
  deviceId: String,
  timeToReachDestination: {
      type: Date,
      index: {
          expires: 900, // documents will expire in 15 minutes after `timeToReachDestination` time
      }
  }
});

const DataModel = mongoose.model('DataModel', DataSchema);

// Middleware to parse incoming JSON data
app.use(express.json());

app.post('/reserveParkingSpot', async (req, res) => {
  const { parkingLotName, deviceId, timeToReachDestination } = req.body;

  try {
      const newData = {
          parkingLotName,
          deviceId,
          timeToReachDestination: new Date(timeToReachDestination)
      };

      const updatedDoc = await DataModel.findOneAndUpdate(
          { deviceId }, 
          newData,
          { upsert: true, new: true, setDefaultsOnInsert: true }
      );

      if(updatedDoc) {
          console.log('Data updated:', updatedDoc);
          res.sendStatus(200);
      } else {
          console.log('Internal server error.');
          res.sendStatus(500);
      }

  } catch (err) {
      console.error('Error:', err);
      res.status(500).send('Internal server error.');
  }
});


app.delete('/reserveParkingSpot', async (req, res) => {
    const tenMinutesAgo = new Date(Date.now() - 10 * 90 * 1000);

    try {
    await DataModel.deleteMany({ timeToReachDestination: { $lt: tenMinutesAgo } });
    console.log('Deleted old data.');
    res.sendStatus(200);
    } catch (err) {
    console.error('Error:', err);
    res.status(500).send('Internal server error.');
    }
});

app.get('parkingLots/:parkingLotName', async (req, res) => {
    const parkingLotName = req.params.parkingLotName;

    try {
    const parkingLotData = await DataModel.find({ parkingLotName });
    const uniqueDevices = new Set(parkingLotData.map((data) => data.deviceId));
    const numberOfUsers = uniqueDevices.size;

    res.status(200).json({
        parkingLotName,
        numberOfUsers
    });
    } catch (err) {
    console.error('Error:', err);
    res.status(500).send('Internal server error.');
    }
});

// Error handling 
app.use((err, req, res, next) => {
    console.error('Error:', err.stack);
    res.status(500).send('Internal server error.');
});

// Start the server
app.listen(port, function(){
    console.log("Server is listening on port: ", port)
});
