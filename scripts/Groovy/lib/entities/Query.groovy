package lib.entities

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class Query {
    List<String> select
    String from
    List<Reduction> reductions
    List<Transformation> transformations
    CompoundFilter where

    // Internal fields. Will be ignored even if specified in the query
    List transformers = []
    Map columns = [:]
}