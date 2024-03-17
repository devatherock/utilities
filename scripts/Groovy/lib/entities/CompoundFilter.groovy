package lib.entities

import groovy.transform.ToString

@ToString(includeNames = true, includeSuperProperties = true, ignoreNulls = true)
class CompoundFilter extends SimpleFilter {
    Operator operator = Operator.AND
    List<CompoundFilter> filters
}