FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
RUN addgroup -S northcare && adduser -S northcare -G northcare
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
