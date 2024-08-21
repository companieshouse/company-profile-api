#!/bin/bash

# Start script for company-profile-api

PORT=8080

exec java -jar -Dserver.port="${PORT}" "company-profile-api.jar"
