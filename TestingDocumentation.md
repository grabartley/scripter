## Testing Scripter

### Unit Tests
We carried out unit tests through Android Studio.


| Test                 | Purpose                                                                               | Status |
|----------------------|---------------------------------------------------------------------------------------|--------|
| ensureHttpInURL()    | To ensure that http:// is automatically appended to the URL if not already present    | Passed |
| ensureJsInFilename() | To ensure that .js is automatically appended to the filename if not already present   | Passed |
| evaluateJS()         | To ensure JavaScript code is interpreted correctly and returns it’s value as a String | Passed |
| addVariableToScope() | To ensure the variable is correctly added to the scope used by JavaScript code        | Passed |
| addFunctionToScope() | To ensure the function is correctly added to the scope used by JavaScript code        | Passed |
| setEqualTo()         | To ensure that the Object arrays are made equal                                       | Passed |
| isEqualTo()          | To ensure that two equal Object arrays will return true                               | Passed |
| getDayOfWeek()       | To ensure that the correct day of the week is returned for an input                   | Passed |
| updateStateByIndex() | To ensure that the correct state is updated to the given value                        | Passed |
| getStateByIndex()    | To ensure that the correct state value is returned                                    | Passed |




### Usability Tests

**Subject Name**
Bernard O’ Connor

**Technical User?**
Yes

**Task**
Download, enable, start and stop a script

**What we expected**
We hoped he would be able to carry out these tasks with ease as a technical user as this is the target audience of our application. We hoped he would complete this task within 1 minute and 30 seconds given that this includes the time taken to type the URL which we provided him with: student.computing.dcu.ie/~bartleg3/scripter/MusicUseCase.js This URL is representative of a URL where a user may store.

**What happened**
Bernard successfully completed these tasks without much trouble but in a longer time than expected. This was mainly due to the time taken to type in the URL of the script to download. Bernard noted that he had pressed the “Start Scripts” button before actually enabling the script he had downloaded and wasn’t aware that the script would not run. He suggested a prompt should be shown to the user if scripts are started when no scripts are enabled.

**Time taken**
2:12.08

**Subject Name**
Edmond Bartley

**Technical User?**
No

**Task**
Start and stop a pre-loaded script periodically over a period of three days

**What we expected**
The purpose of this test was to examine the long term usage of Scripter by a non-technical user. We expected Edmond to be able to start and stop a pre-loaded script each day and keep it running for a period of 8 hours in the background on his phone.

**What happened**
Edmond was able to start and stop the script each day but was unable to keep it running in the background for 8 hours on his phone due to his battery being drained. He commented that our application was very power-intensive but worked as expected other than that.

**Time taken**
Three days


