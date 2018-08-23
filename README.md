# react-native-cipherlab-scanner

Use react-native on your Android based CipherLab scanner!

## Supported Models:
CipherLab RS31

## Getting started

```bash
$ react-native init --version="0.55.4" AwesomeScanner
$ cd AwesomeScanner 
$ npm install
$ npm install react-native-cipherlab-scanner --save
```

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import au.com.micropacific.react.cipherlab.CipherLabScannerPackage;` to the imports at the top of the file
  - Add `new CipherLabScannerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-cipherlab-scanner'
  	project(':react-native-cipherlab-scanner').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-cipherlab-scanner/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-cipherlab-scanner')
  	```


## Usage
```javascript
// App.js
import React, {Component} from 'react';
import {Platform, StyleSheet, Text, View} from 'react-native';


import CipherLabScannerModule from 'react-native-cipherlab-scanner';
import { DeviceEventEmitter, Button } from 'react-native';

const instructions = Platform.select({
  ios: 'Press Cmd+R to reload,\n' + 'Cmd+D or shake for dev menu',
  android:
    'Double tap R on your keyboard to reload,\n' +
    'Shake or press menu button for dev menu',
});

type Props = {};
export default class App extends Component<Props> {
  constructor(props)
  {
    super(props);

    this.state = {
      barcode: '',
      type: '',
      binary: '',
    }
    
    this.requestScan=this.requestScan.bind(this); 

    // MDR 21/08/2018 - make fake scans if we're on an emulator
    var _this = this;
    this.scanner = {
      requestScan: function() {
        _this.barcodeReadEvent({barcode: '123', type: 'FAKE_BARCODE', binary: []})
      }
    }
    
    this.listeners = [];
  }

  async componentDidMount() {
    this.listeners.push(DeviceEventEmitter.addListener('CIPHERLAB.initEvent', this.scannerInitEvent.bind(this)));
    this.listeners.push(DeviceEventEmitter.addListener('CIPHERLAB.barcodeReadEvent', this.barcodeReadEvent.bind(this)));

    await CipherLabScannerModule.initialise();

    // MDR 21/08/2018 - Note that this incurs a performance hit for each scan
    await CipherLabScannerModule.enableBinaryData();
  }

  async componentWillUnmount() {
    console.log(`componentWillUnmount()`);

    for (var x in this.listeners) {
      this.listeners[x].remove();
    }

    this.listeners = [];
  }
  
  requestScan(e) {
    //CipherLabScannerModule.requestScan();
    this.scanner.requestScan();
  }

  async scannerInitEvent(e) {
    // MDR 21/08/2018 - The onboard scanner has been initialised, overwrite our fake scanner
    console.log(`CIPHERLAB.initEvent initcallback`);
    this.scanner = CipherLabScannerModule;
  }

  async barcodeReadEvent(e) {
    this.setState({barcode: e.barcode, type: e.type, binary: e.binary});
  }

  render() {
    return (
      <View style={styles.container}>
        <Text style={styles.welcome}>Welcome to React Native!</Text>
        <Text style={styles.instructions}>To get started, edit App.js</Text>
        <Text style={styles.instructions}>{instructions}</Text>
        <Text style={styles.instructions}>Barcode: {this.state.barcode}</Text>
        <Text style={styles.instructions}>Type: {this.state.type}</Text>
        <Text style={styles.instructions}>Binary: {this.state.binary}</Text>        
        <Button title="Scan" capitalize={false} block primary onPress={this.requestScan} />
      </View>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#F5FCFF',
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
});
```
  