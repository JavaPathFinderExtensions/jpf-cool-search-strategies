# jpf-cool-listeners

This repo is a series of listener extensions for checking progress while jpf is searching.More listeners are expected to be added. 

### Install 

- Clone this repo under jpf home.  The recommended structure is:

  - jpf-core
  - jpf-cool-listeners
  - ...

- Add `jpf-progress` inside `~/.jpf/site.properties`, eg:

  ```properties
  ...
  jpf-cool-listeners = ${jpf.home}/jpf-cool-listeners
  extensions+=,${jpf-cool-listeners}
  ```

  

- Inside `jpf-cool-listeners`, do `ant build`



### Usage

- Inside the `.jpf` file you are trying to run, add 

```properties
@using = jpf-cool-listeners
listener = ${listener-you-wanna-use}
...

```



### Reference

- `PathCountEstimator.java` credit to: [K. Wang, H. Converse, M. Gligoric, S. Misailovic, and S. Khurshid. A progress bar for the JPF search using program executions. In JPF, 2018.](https://dl.acm.org/citation.cfm?id=3302419)

  

