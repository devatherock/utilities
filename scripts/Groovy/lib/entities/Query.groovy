package lib.entities

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class Query {
    List<String> select
    String from
    List<Reduction> reductions
    List<Transformation> transformations
    List transformers = []
    CompoundFilter where
}