# 프론트엔드를 빌드해서 Nginx로 서빙 + /api는 backend 컨테이너로 리버스 프록시하는 게이트웨이 이미지.
# 빌드 컨텍스트는 저장소 루트(..)여야 한다 (frontend/ 와 infra/nginx/nginx.conf를 모두 COPY해야 하므로).
FROM node:20-alpine AS build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ .
# 같은 오리진(이 Nginx)을 통해 /api/**를 상대경로로 호출하도록 빌드한다 (CORS 자체가 필요 없어짐).
ENV VITE_API_BASE_URL=""
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY infra/nginx/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
