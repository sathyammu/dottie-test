# Use an official Java runtime as a parent image
FROM mcr.microsoft.com/openjdk/jdk:21-ubuntu

# Define Playwright browsers path
ENV PLAYWRIGHT_BROWSERS_PATH=/pw-browsers
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=true

# Create the directory
RUN mkdir -p $PLAYWRIGHT_BROWSERS_PATH

#Enable ssh vide https://learn.microsoft.com/en-us/azure/app-service/configure-custom-container?pivots=container-linux&tabs=debian#detect-https-session
RUN apt-get update \
    && apt-get install -y --no-install-recommends dialog \
    && apt-get install -y --no-install-recommends openssh-server \
    && echo "root:Docker!" | chpasswd

# Install Node.js
RUN apt-get update && \
    apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash - && \
    apt-get install -y nodejs

# Use a build argument for Playwright version
ARG PLAYWRIGHT_VERSION=latest

EXPOSE 80 2222

# Install Playwright
RUN npm install -g playwright@$PLAYWRIGHT_VERSION

RUN npx playwright install-deps

# Initialize Playwright browsers
RUN npx playwright install chromium
