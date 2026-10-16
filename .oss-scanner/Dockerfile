# Anthropic OSS Scanner build image for Fern (Android / Kotlin).
# Network is available during this build only; the security agent runs offline afterward.
FROM debian:bookworm

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk

RUN apt-get update && apt-get install -y --no-install-recommends \
      ca-certificates curl unzip git openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdtools.zip \
    && unzip -q /tmp/cmdtools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdtools.zip \
    && yes | sdkmanager --licenses >/dev/null \
    && sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# scanner contract: checkout at /src
COPY . /src
WORKDIR /src

RUN chmod +x gradlew \
    && ./gradlew assembleDebug --no-daemon --stacktrace \
    || (echo "WARNING: assembleDebug reported issues; sources remain available for offline audit" && true)
