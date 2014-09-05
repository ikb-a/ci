# Contributional Implementation

A library for implenting contributional implementations (CIs) in Java.

## Setup
`ci` is a standard maven project. `ci` depends on at least Java 8 or later.

## Documentation
Most of the documentation can be found in inline JavaDoc comments. A very high
level overview can be found in this file.

### Generic Arguments
Anywhere you find a generic in the `ci` package, it is generally the case that
the following are what they stand for:

- `<I> -> Input Type to Source/CI`
- `<O> -> Output Type from Source/CI`
- `<T> -> Trust Type`
- `<Q> -> Quality Type`

### CI creation
A CI is created by using one of the CI constructors. You need to pass in a
`Contract` or a list of `Source`s, as well as a `Selector`, `Aggregator` and
optionally a `Acceptor`.

### Source Creation
A source is defined by subclassing either `Source` or `BasicSource`. To make a
source implement a `Contract`, simply add the contract as an interface, and
register the source with `Contracts.register()`.

### Adaptors
An adaptor is defined by subclassing either `Adaptor` or `BasicAdaptor`. More
information about Adaptors can be found in the docstrings.

### Source Discovery
Sources are discovered by using `Contracts.discover()`. All sources have to be
registered with `Contracts.register()` before this is invoked, in order for it
to work correctly.

This will return a list of sources, which can be passed into the CI constructur.
Additionally, when passed a `Class<? extends Contract>` instead of a list of
sources, the CI constructor will perform `Contracts.discover()` to discover the
sources to use to implement the CI.

#### Contracts
Contracts are implemented by creating a non-generic sub-interface of the `Contract`
interface. This interface can then be implemented by `Adaptor`s and `Source`s.

### Budgets and Costs
A budget is represented by a `Allowance[]`, and costs are represented by
`Expenditure[]`. By default, there are a set of allowances and expenditures
implemented in `edu.toronto.cs.se.ci.budget.basic`, including `Flag`s, `Dollar`s,
and `Time`.

## Next Steps
 - There should be a mechanism for easily adding memoization/disk-caching to sources, to reduce
   the number of times a single source has to hit the network and allow for resuming

 - There should be a mechanism for saving the state of a CI and resuming it at another point
   or on another computer

 - Aggregation is simply a more general version of map-reduce, and each source is 
   (by definition) independent. There should be a mechanism to massively parallelize the 
   source execution and aggregation process in a similar way to map-reduce

 - Error handling and cases of uncertainty are not handled very well by the current system,
   work should be done to improve the state of things

 - Consider moving away from Java and to a language with better first class function support,
   as CIs tend to be fairly functional in nature.

 - The system has no tests to verify the correctness of the overall implementation or the 
   implementations of any aggregators. Tests should be written to do those things
