# Build context is ./frontend (see docker-compose.yml) - this one doesn't
# need the repo root since it has no multi-module Maven dependency to see.

FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# Vite bakes VITE_* variables into the built JS at BUILD time, not runtime -
# unlike the backend services, you can't change this after the image is
# built without rebuilding. docker-compose.yml passes this as a build arg
# pointing at the Gateway's HOST-published port (localhost:8080), because
# this code runs in the user's browser, not inside the Docker network -
# the browser can't resolve container hostnames like "api-gateway".
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
