#!/bin/bash
# Start Nginx load balancer
# Routes requests from port 8080 to Spring Boot instances on ports 8081 and 8082
# with round-robin distribution

nginx -c /mnt/d/workspace/address-j/nginx.conf
