---
title: Building Metabase
---

# Building Metabase

Here you will learn how to run Metabase on a local server for further development

## Install the prerequisites

To complete any build of the Metabase code, you'll need to install the following.

1. [Clojure (https://clojure.org)](https://clojure.org/guides/getting_started) - install the latest release by following the guide depending on your OS. 
Or use the command 

```
sudo apt-get install leiningen
```

2. [Java Development Kit JDK (https://adoptopenjdk.net/releases.html)](https://adoptopenjdk.net/releases.html) - you need to install JDK 11. Or use the command

```
sudo apt install openjdk-11-jdk
```

3. [Node.js (http://nodejs.org/)](http://nodejs.org/) - latest LTS release. Or use the commands

```
curl -sL https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh -o install_nvm.sh
bash install_nvm.sh
source ~/.profile
nvm install --lts
```

4. [Yarn package manager for Node.js](https://yarnpkg.com/) - latest release of version 1.x - you can install it in any OS by running:

```
npm install --global yarn
```

## Run

1. Go to the root of the project

2. Now we’ll start up the backend server of Metabase with:

   ```
   clojure -M:run
   ```

   When it’s done, you should see a message that says something like “Metabase initialization complete.” Keep this tab in your terminal app running, otherwise it’ll stop Metabase.

3. Open up another tab or window of your terminal app, and then “build” the frontend (all the UI) with this command:

   ```
   yarn build-hot
   ```

If you're having trouble with this step, make sure you are using the LTS version of [Node.js (http://nodejs.org/)](http://nodejs.org/).


4. In your web browser of choice, navigate to `http://localhost:3000`, where you should see Metabase!

   This is the local “server” on your computer, and 3000 is the “port” that Metabase is running on. You can have multiple different apps running on different ports on your own computer. Note that if you share any URLs with others that begin with `localhost`, they won’t be able to access them because your computer by default isn’t open up to the whole world, for security.

## Shutting down Metabase

If you want to make Metabase stop running, you can either quit your terminal program, or go to the tab with the backend running and hit `Ctrl+C` to stop the backend.