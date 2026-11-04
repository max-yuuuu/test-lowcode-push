# 前端镜像：Vite 构建 → nginx 静态托管并反向代理 /api
# 构建上下文为 scaffold-vue 目录：物料包 @yuanda-lowcode-ai/scaffold-ui 是普通 npm 依赖，不需要 workspace 清单
# 交付项目不带锁文件（锁跟着平台的 workspace 走），按 package.json 里的版本号 npm install
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
