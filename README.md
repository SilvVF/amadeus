# amadeus
Amadeus is a Manga dex manga reader that supports offline reading and browsing of manga.
Features are based on [Tachiyomi]("https://tachiyomi.org/") and [Neko]("https://tachiyomi.org/forks/Neko/"). 

#tech stack
 [Koin](https://insert-koin.io/)
 [Room]("https://developer.android.com/jetpack/androidx/releases/room")
 [Voyager]("https://voyager.adriel.cafe/")
 [Datastore]("https://developer.android.com/jetpack/androidx/releases/datastore")
 [Sandwich]("https://github.com/skydoves/sandwich")
 [Ktor Client]("https://ktor.io/")
 [OkHttp]("https://square.github.io/okhttp/")
 [Okio]("https://square.github.io/okio/")
 [Compose]("https://developer.android.com/jetpack/compose")
 [WorkManager]("https://developer.android.com/topic/libraries/architecture/workmanager")
 [Tachiyomi Image Decoder]("https://github.com/tachiyomiorg/image-decoder")
 [Coil]("https://coil-kt.github.io/coil/")
 [Coroutines / Flow]("https://kotlinlang.org/docs/coroutines-overview.html")
 [Paging 3]("https://developer.android.com/topic/libraries/architecture/paging/v3-overview")
 [kotlin serialization]("https://kotlinlang.org/docs/serialization.html")

# modules
## core
The core module contains the business logic for the app. It is split into modules that contain diffrent parts of the business logic.
### datastore
This module contains all of the logic for storing data withing Preferences datastore and only depends on the core:common module.
### common 
This module contains common logic and models needed by all of the other modules within core. It depends on no outside modules
### data 
This module contains the apps repositories and logic for fetching and downloading manga. This also contains the logic for accessing files on the device. The data module can depend on all other modules inside of the core module excepte for the core:domain module.
### domain
The domain module is responsible for providing @Stable types and contains the compose runtime dependecy to do this. it also contains logic for accessing the data layer logic from feature modules.
### database
The database module contains the Room DB Daos and Entities.
### network
The network module contains the logic for making requests to Mangadex api.
### navigation
Provides SharedScreen types to be registered in the application for multi module navigation using Voyager.
### ui
Conatains shared ui components and types.

## feature
module for containing the diffrent app feature submodules.
### explore
contains the screen for browsing manga.
### manga
contains the screens for viewing specific manga and for viewing categories of manga.
### reader
contains the manga reader implementation.
### library
contains the library browsing screens.



![Screenshot_20231218_180748_amadeus](https://github.com/SilvVF/amadeus/assets/98186105/fe8a3c5f-6bdf-4a1e-8372-4d72ebac3f9a)
![Screenshot_20231218_180625_amadeus](https://github.com/SilvVF/amadeus/assets/98186105/11e93865-a300-44a3-8bb2-cdf7d4b81ae1)
![Screenshot_20231218_180538_amadeus](https://github.com/SilvVF/amadeus/assets/98186105/4d49452b-3ea2-43f0-b5fc-f382e80f7b19)
![Screenshot_20231218_180511_amadeus](https://github.com/SilvVF/amadeus/assets/98186105/89b942e8-f0d4-46ed-bd34-cbded5324479)
