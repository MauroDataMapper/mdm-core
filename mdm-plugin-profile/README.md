# mdm-plugin-profile

| Branch | Build Status |
| ------ | ------------ |
| master | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-profile%2Fmaster)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-profile/branches) |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-profile%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-profile/branches) |

## Requirements

* Java 12 (AdoptOpenJDK)
* Grails 4.0.3+
* Gradle 6.5+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).


## Url Mappings Report

```
Controller: profile
 |   GET    | /api/profiles/providers                                         | Action: profileProviders
 |   GET    | /api/profiles/${profileNamespace}/${profileName}/models/values  | Action: listValuesInProfile
 |   GET    | /api/profiles/${profileNamespace}/${profileName}/models         | Action: listModelsInProfile
 |   POST   | /api/profiles/${profileNamespace}/${profileName}/search         | Action: search

 |   POST   | /api/profiles/${profileNamespace}/${profileName}/${catalogueItemDomainType}/${catalogueItemId}                    | Action: save
 |   GET    | /api/profiles/${profileNamespace}/${profileName}/${catalogueItemDomainType}/${catalogueItemId}                    | Action: show
 |   POST   | /api/${catalogueItemDomainType}/${catalogueItemId}/profile/${profileNamespace}/${profileName}/${profileVersion}?  | Action: save
 |   GET    | /api/${catalogueItemDomainType}/${catalogueItemId}/profile/${profileNamespace}/${profileName}/${profileVersion}?  | Action: show
```