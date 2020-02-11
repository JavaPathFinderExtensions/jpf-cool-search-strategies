# jpf-cool-jpf-cool-search-strategies

This repo is a series of search strategies. Basic PCT search is implemented

### Install 

- Clone this repo under jpf home.  The recommended structure is:

  - jpf-core
  - jpf-cool-search-strategies
  - ...

- Add `jpf-cool-search-strategies` inside `~/.jpf/site.properties`, eg:

  ```properties
  ...
  jpf-cool-search-strategies = ${jpf.home}/jpf-cool-search-strategies
  extensions+=,${jpf-cool-search-strategies}
  ```

  

- Inside `jpf-cool-search-strategies`, do `ant build`



### Usage



#### StatelessPCTSearch

- Inside the `.jpf` file you are trying to run, add 

```properties
@using = jpf-cool-search-strategies
search.class = .search.StatelessPCTSearch
# optional params based on SUT:
search.StatelessPCTSearch.path_limit = ...
search.StatelessPCTSearch.n = ...
search.StatelessPCTSearch.numPrioritySwitchPoints = ...
search.StatelessPCTSearch.maxPrioritySwitchPoints = ...
```

In regards of core `PCT`, here's an excerpt adapted from the reference paper

![634DA21A-D919-4F55-B1E5-29CBE0FA6A05](img_pct.png)

#### DelayBoundedDFS

```properties
@using = jpf-cool-search-strategies
search.class = gov.nasa.jpf.search.DelayBoundedDFS
search.DelayBoundedDFS.delayBudget = ...
# If set to true, exploring non-scheduling choices will not cost delay budget
search.DelayBoundedDFS.scheduleCostOnly = ...
```





### Reference

- PCT strategy: [Sebastian Burckhardt, Pravesh Kothari, Madanlal Musuvathi, and Santosh Nagarakatte. A randomized scheduler with probabilistic guarantees of finding bugs.](https://dl.acm.org/citation.cfm?id=1736040)

- DelayBoudedDFS: [Systematic Testing of Asynchronous Reactive Systems](https://dl.acm.org/doi/10.1145/2786805.2786861)

  

