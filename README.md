# Family Gem
### _Create your own family tree_

Family Gem is an app for Android designed to manage family trees.

## Features
With Family Gem you can:
- Create a family tree from scratch, entering names, dates, places, various events, photos and sources.
- Import an existing family tree through a GEDCOM file and modify it as you want.
- Export the family tree you created (via GEDCOM again) to import in every other genealogy program.
- Share a tree with your relatives, letting them improve it and receiving back the updates. Then you can choose whether accept them or not.

The intent is that data structure respects as much as possible the latest version of GEDCOM standard: [5.5.1](https://www.familysearch.org/developers/docs/gedcom/) and possibly also [5.5.5](https://www.gedcom.org/gedcom.html).<br>
Family Gem is strongly based on the library [Gedcom 5 Java](https://github.com/FamilySearch/gedcom5-java) by FamilySearch.

## Limitations
The code provided in this repository should compile and build a working version of Family Gem, but with some limitations:
|Missing|Limitation|
|-|-|
|App signature|You loose saved trees when you install over a signed version|
|Server account|You can't share trees|
|GeoNames account|Place names suggestions probably don't appear|

## License
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
