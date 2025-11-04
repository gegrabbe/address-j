#!/bin/bash
# Stop Nginx load balancer gracefully
# Uses the same config file to identify which Nginx instance to stop

nginx -c /mnt/d/workspace/address-j/nginx.conf -s stop
