# CouchDB deployment and upgrade

The reference stacks use CouchDB 3.5.3 with authentication. They require an
application image containing the `SWARMPIT_DB_USERNAME` and
`SWARMPIT_DB_PASSWORD_SECRET` settings; older Swarmpit images cannot use these
settings. The package installer has its own templates and is not updated by
changing this repository's Compose files.

## New installations

On a Swarm manager, create the external password secret before deploying:

```sh
openssl rand -hex 32 | docker secret create swarmpit_db_password -
docker stack deploy -c docker-compose.yml swarmpit
```

Use `docker-compose.arm.yml` for ARM64. The official CouchDB 3 image supports
AMD64 and ARM64, not ARMv7/32-bit ARM. For an ARMv7 cluster, run CouchDB on a
supported host and configure `SWARMPIT_DB` with its address and the corresponding
credentials; do not substitute an obsolete CouchDB image.

Both the app and database receive the same secret. The CouchDB administrator is
`swarmpit`; its password is read inside the container, so it is not embedded in
the service environment, Compose YAML or database URL. The image's entrypoint
still initializes CouchDB and drops privileges. `couchdb.ini` requires
authentication even for databases inherited from CouchDB 2, whose security
objects may otherwise allow anonymous access. Port 5984 is not published.

The secret name is shared across the two reference files. If deploying separate
instances that need independent credentials, change the secret name and its
references together. Swarm secrets are immutable: rotate by creating a new secret
and updating both services during a maintenance window. The reference stack
persists data, not CouchDB's generated administrator configuration.

The database has a 512 MB memory limit and a 256 MB reservation. `ERL_FLAGS=+S 2:2`
bounds Erlang's scheduler allocation on hosts with many CPUs. These are defaults
for Swarmpit's application database, not a guarantee for every workload; monitor
memory and adjust the limit and reservation together if needed.

## Existing CouchDB 2 installations

This is a database major-version upgrade. Schedule downtime and test the upgrade
against a copy of your data first. Follow the
[official single-node upgrade guide](https://docs.couchdb.org/en/stable/install/upgrading.html#upgrading-from-couchdb-2-x).
Do not deploy this Compose file blindly over an existing stack.

1. Save the current Compose file, exact app/database image digests, replica counts,
   CouchDB configuration and volume information. Determine which node holds the
   database's local volume. Before redeploying, constrain `db` to that node (or use
   your existing shared-volume placement). A volume with the same name on another
   node is a different, empty database.
2. Scale the app to zero to stop writes, then scale `db` to zero and wait for its
   task to stop. Take a cold backup of the **entire** database volume on its owning
   node, including hidden files, shard metadata and system databases, plus any
   custom CouchDB configuration. Verify that the backup can be restored. Retain
   the backup outside the volume being upgraded.
3. Create the password secret on the manager as above. Keep the existing stack
   name, database volume and volume driver. Merge any custom CouchDB settings into
   the new configuration; the reference configuration uses a single replica
   (`[cluster] n = 1`) and requires valid users.
4. Deploy the updated Compose file with the new application image and CouchDB
   3.5.3. This starts the services again with the recorded data volume and shared
   credentials. Do not run an old and new CouchDB process against the same volume.
5. Check database and app task logs, log in using your existing **Swarmpit**
   account, and verify users, API tokens, linked registries, stored stack YAML and
   history. CouchDB administrator credentials are separate from Swarmpit accounts.
   Confirm unauthenticated database requests return 401. Keep the cold backup
   until the deployment has been validated.

### Rollback

Stop the upgraded app and database. Restore the pre-upgrade cold backup and saved
configuration into a separate empty volume on the original node, then redeploy
the saved old Compose file and image digests pointing at that restored volume.
Do not simply point CouchDB 2 at files already written or compacted by CouchDB 3.
Writes made after the backup require a separate recovery plan and are not
preserved by this rollback.

## Development

Before creating a new development database or starting Figwheel, export:

```sh
export SWARMPIT_DB_USERNAME=swarmpit
export SWARMPIT_DB_PASSWORD="$(openssl rand -hex 32)"
```

Keep these values for subsequent application starts against that database.
`dev/script/init-db.sh` creates CouchDB 3.5.3, binds it to loopback, and limits it
to 512 MB. It never upgrades or deletes an existing `swarmpitdb` container.
Back up existing development data before replacing an older container; existing
unauthenticated configurations remain supported when the new settings are unset.
