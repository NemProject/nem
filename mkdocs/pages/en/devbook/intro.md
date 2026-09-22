---
title: Welcome
---

# Welcome to the Developer Manual

The developer manual is for developers building applications on NEM.
It provides code examples in multiple programming languages,
showing how to perform common tasks with the <SDK:> or the HTTP API.

The manual is structured as follows:

<div class="icon-list" markdown>

* :material-laptop: **Getting Started**

    Set up your development machine and run a quick `Hello World` sample to check that everything is ready.

* :material-school: **Tutorials**

    Follow task-focused tutorials grouped by area.
    Each tutorial links to the [textbook](../textbook/intro.md) and the relevant reference guides when background
    information is useful.

* :material-book-open-page-variant: **Reference Guides**

    Consult exhaustive information about SDK methods, HTTP and WebSockets endpoints, and binary structures.

</div>

Use the navigation menu, or jump directly into one of the tutorials below.

Tutorials are grouped by level, from beginner to advanced, based on the required familiarity with NEM concepts.

{% import 'tutorials_table.jinja2' as tutorials_table with context %}

{{ tutorials_table.render() }}
