# ============================================
# ÉTAPE 1 : BUILD ANGULAR
# ============================================
FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci --legacy-peer-deps

COPY . .

RUN npm run build -- --configuration production


# ============================================
# ÉTAPE 2 : NGINX
# ============================================
FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf

# ⚠️ Adjust this path to match your Angular version's output
COPY --from=build /app/dist/sirh-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]