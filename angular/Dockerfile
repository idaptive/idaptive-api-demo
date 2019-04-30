FROM nginx:latest
 
COPY default.conf /etc/nginx/conf.d/
COPY <YOUR_CERTIFICATE_NAME> /etc/ssl/
COPY <YOUR_CERTIFICATE_KEY> /etc/ssl
WORKDIR /usr/share/nginx/html
COPY dist/api-demo . 