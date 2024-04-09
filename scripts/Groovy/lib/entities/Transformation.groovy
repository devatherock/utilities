package lib.entities

import groovy.transform.ToString

@ToString(includeNames = true, ignoreNulls = true)
class Transformation {
    String name
    List<String> fields
    Map<String, String> parameters
}