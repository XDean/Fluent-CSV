# Fluent CSV
[![Build Status](https://travis-ci.org/XDean/Fluent-CSV.svg?branch=master)](https://travis-ci.org/XDean/Fluent-CSV)
[![codecov.io](http://codecov.io/github/XDean/Fluent-CSV/coverage.svg?branch=master)](https://codecov.io/gh/XDean/Fluent-CSV/branch/master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.XDean/fluent-csv/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.XDean/fluent-csv)

Fluent and Flexible CSV Framework

# Get it 

```xml
<dependency>
    <groupId>com.github.XDean</groupId>
    <artifactId>fluent-csv</artifactId>
    <version>1.x</version>
</dependency>
```

# [Sample](src/test/java/xdean/csv/Sample.java)

You have a space-delimited file:

*people.csv*

```csv
id name desc
1 Mike 'Football&Basketball'
2 Dan 'Swim and walk'
3 Alex  
```

We can define domain class:

*Person.java*

```java
@CsvConfig(splitor = ' ', quoter='\'')
class Person{
  @CSV
  int id;
  
  @CSV
  String name;
  
  @CSV(name="desc", optional=true, defaultValue="No Description")
  String description;
}
```

Then read data by `FluentCSV`

```java
FluentCSV.create()
  .readConfig(Person.class) // read the class's CSV config
  .readBean(Person.class) // read as bean
  .from(Paths.get("people.csv")) // from the file
  .forEach(System.out::println);
```

You will get:

```
Sample.Person(id=1, name=Mike, description=Football&Basketball)
Sample.Person(id=2, name=Dan, description=Swim and walk)
Sample.Person(id=3, name=Alex, description=No Description)
```
