FROM node:20-alpine

WORKDIR /docs

# Copy package manifests first — this layer is cached until they change
COPY package.json package-lock.json ./
RUN npm ci

EXPOSE 7006

CMD ["npm", "run", "start", "--", "--host", "0.0.0.0", "--port", "7006"]
