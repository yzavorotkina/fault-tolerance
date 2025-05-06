#!/bin/bash

echo "Initializing replicaset (if not already)..."

mongosh --host mongo-primary:27017 <<EOF
var status = rs.status();
if (!status.ok) {
  var cfg = {
    _id: "rs0",
    members: [
      { _id: 0, host: "mongo-primary:27017" },
      { _id: 1, host: "mongo-secondary1:27017" },
      { _id: 2, host: "mongo-secondary2:27017" }
    ]
  };
  rs.initiate(cfg);
  print("Replica set initialized.");
} else {
  print("Replica set already initialized.");
}
EOF

echo "Waiting for PRIMARY election..."
until mongosh --host mongo-primary:27017 --quiet --eval "rs.isMaster().ismaster" | grep "true"; do
  echo "Waiting for primary..."
  sleep 2
done

echo "Creating admin user (if not exists)..."
mongosh --host mongo-primary:27017 <<EOF
use admin;
if (db.getUser("admin") == null) {
  db.createUser({
    user: "admin",
    pwd: "secret",
    roles: [ { role: "root", db: "admin" } ]
  });
  print("Admin user created.");
} else {
  print("Admin user already exists.");
}
EOF

echo "Replica set and user setup complete."
