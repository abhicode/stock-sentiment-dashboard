# Stage 1: Build React app
FROM node:20 AS build

# Set working directory
WORKDIR /app

ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Install dependencies
COPY package.json package-lock.json ./
RUN npm install

# Copy all source code
COPY . .

# Build the app for production
RUN npm run build --verbose

# Stage 2: Serve app with Nginx
FROM nginx:alpine

# Remove default nginx page and copy build output
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy custom nginx config if needed (optional)
# COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]