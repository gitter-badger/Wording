# Wording

## Setup
```bash
gulp

# For iOS app
cd ios/wording
pod install
open wording.xcworkspace
```

### How to install gulp & required plugins
```bash
npm install -g gulp
npm install --save-dev gulp
npm install run-sequence
npm install gulp-sass
npm install gulp-shell
```
### Other required plugins
```bash
npm install ng-dialog
```

#### How to only use api
```bash
cd api
./initialize.sh
python3 api.py

# In another terminal window
curl http://127.0.0.1:5000/cor
```

#### How to only use web
```bash
# Start api server first
cd web
. ../api/env/bin/activate
python3 site.py

# Go to site
open http://127.0.0.1:5001/
```

#### How to use the login system in API
```bash
# First start the api
cd api
./initialize.sh
python3 api.py

# For registering users, do:
curl -i -X POST -H "Content-Type: application/json" -d '{"username":"username","password":"password","email":"valid_email"}' http://127.0.0.1:5000/register
```

#### How to contribute to translations
You can add translations to the translation file which is located at 'web/templates/translations.json'.
The iso codes are in the following format: ISO 639-2B, more information on [this wikipedia link](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes).
