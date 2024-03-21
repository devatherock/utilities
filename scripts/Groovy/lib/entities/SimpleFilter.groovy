package lib.entities

class SimpleFilter {
    String field
    Condition condition = Condition.IN
    List<Object> values
}