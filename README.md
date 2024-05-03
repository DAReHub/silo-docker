# SILO Docker Implementation

Forked from: https://github.com/msmobility/silo

## Setting up a new location
Use Maryland use case as a template and file structure example. \
...

## Edit .properties
- `base.directory`: path to scenario from within container, e.g. `./data/scenarios/annapolis/`

## Edit docker-compose
- `PROPERTIES_FILE`: .properties filepath from within container, e.g. `/opt/silo/data/scenarios/annapolis/javaFiles/siloMatsim_multiYear.properties`
- `CONFIG_FILE`: config.xml filepath from within container, e.g. `/opt/silo/data/scenarios/annapolis/matsim_input/config.xml`
- `USE_CASE`: name of the use case directory, e.g. maryland. Requires rebuilding image.
- `source`: local directory for input data, e.g. `${PWD}/useCases/maryland/test`

## Build and run
    docker compose build
    docker compose up

## Outputs
Outputs can be found within generated `scenOutput` file within your scenario.