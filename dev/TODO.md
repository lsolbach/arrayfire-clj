# TODOs

## General
* decide where to add parameter checks (integration/API layer)?

## FFI Layer
* define aliases for typedefs/structs

## Integration Layer
* move functions from core to integration layer?
  * dtype-next integration, array-from-tensor, array conversions
  * keyword conversions
* move Arena handling mechanism from core to integration layer
  * make sure, the configured arena is used for all memory allocations

## API Layer
* add within-arrayfire? predicate
  * check for within-arrayfire in API functions
* add API functions
* add specs for API functions

## Notebooks

## README.md
* update examples to use Clojure API

## AGENTS.md
* update Clojure API layer guidelines