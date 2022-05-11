# Contributing to nem.deploy

First off, thank you for considering contributing to nem.deploy.

nem.deploy is an open source project and we love to receive contributions from
our community — you! There are many ways to contribute, from writing tutorials or blog
posts, improving the documentation, submitting bug reports and feature requests or
writing code which can be incorporated into nem.deploy itself.

Following these guidelines helps to communicate that you respect the time of
the developers managing and developing this open source project. In return,
they should reciprocate that respect in addressing your issue, assessing changes,
and helping you finalize your pull requests.

Please, **don't use the issue tracker for support questions**.

## Bug reports

If you think you have found a bug in nem.deploy, first make sure that you
are testing against the latest version of nem.deploy - your issue may already
have been fixed. If not, search our issues list on GitHub in case a similar
issue has already been opened.

It is very helpful if you can prepare a reproduction of the bug. In other words,
provide a small test case which we can run to confirm your bug. It makes it easier to
find the problem and to fix it.

Please, take in consideration the following template to report your issue:

> **Expected Behavior**\
> Short and expressive sentence explaining what the code should do.
>
> **Current Behavior**\
> A short sentence enplaning what the code does.
>
> **Steps to reproduce**\
> For faster issue detection, we would need a step by step description do reproduce the issue.

Provide as much information as you can.

Open a new issue [here][github-issues].

## Contributing code and documentation changes

If you have a bugfix or new feature that you would like to contribute to nem.deploy, please find or open an issue
about it first. Talk about what you would like to do. It may be that somebody is already working on it, or that there
are particular issues that you should know about before implementing the change.

We enjoy working with contributors to get their code accepted. There are many approaches to fixing a problem and it is
important to find the best approach before writing too much code.

### Fork and clone the repository

You will need to fork the main nem.deploy code or documentation repository and clone
it to your local machine. See [github help page](https://help.github.com/articles/fork-a-repo/) for help.

### Submitting your changes

1. Make sure your code meets coding style (Find the rules in [`eclipse-formatter.xml`](eclipse-formatter.xml)).
2. Run the lint tool `mvn spotless:check` and `mvn spotless:apply`.
3. Code without corresponding tests will not be accepted.
4. Test your changes, run tests to make sure that nothing is broken.
5. Use github's *pull request* feature to submit your proposed changes.

Then sit back and wait. There will probably be discussion about the changes and, if any are needed, we would love to work with you to get them merged into nem.deploy.

*CONTRIBUTING.md is based on [CONTRIBUTING-template.md](https://github.com/nayafia/contributing-template/blob/master/CONTRIBUTING-template.md)* , [elasticsearch/CONTRIBUTING](https://github.com/elastic/elasticsearch/blob/master/CONTRIBUTING.md) and [spark/CONTRIBUTING](https://github.com/apache/spark/blob/master/CONTRIBUTING.md).

[github-issues]: https://github.com/NemProject/nem.deploy/issues
