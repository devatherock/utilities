package lib.entities

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class Query {
    List<String> select
    List<Reduction> reductions
    CompoundFilter where
}