FROM openjdk
ADD target/bermedanobot.rocks-0.0.1-SNAPSHOT-fat.jar bot.jar
ENTRYPOINT ["java", "-jar", "/bot.jar", "-DBOT_USER=$BOT_USER", "-DBOT_TOKEN=$BOT_TOKEN", "-DIMGFLIP_PWD=$IMGFLIP_PWD", "-DIMGFLIP_USR=$IMGFLIP_USR"]
