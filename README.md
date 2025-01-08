# The Advanced Android Camera
<a id="readme-top"></a>

<!-- PROJECT SHIELDS -->
<!--
*** I'm using markdown "reference style" links for readability.
*** Reference links are enclosed in brackets [ ] instead of parentheses ( ).
*** See the bottom of this document for the declaration of the reference variables
*** for contributors-url, forks-url, etc. This is an optional, concise syntax you may use.
*** https://www.markdownguide.org/basic-syntax/#reference-style-links
-->
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![project_license][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]
[![CodeFactor](https://www.codefactor.io/repository/github/thephoneproject/advanced-android-camera/badge)](https://www.codefactor.io/repository/github/thephoneproject/advanced-android-camera)



<!-- PROJECT LOGO -->
<br />
<div align="center">
  <a href="https://github.com/ThePhoneProject/advanced-android-camera">
    <img src="/logo.png" alt="Logo" width="80" height="80">
  </a>

<h3 align="center">The Advanced Android Camera</h3>

  <p align="center">
    project_description
    <br />
    <a href="https://github.com/ThePhoneProject/advanced-android-camera"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://www.youtube.com/shorts/AVpb-UuDKwU">View Demo on YouTube</a>
    ·
    <a href="https://github.com/ThePhoneProject/advanced-android-camera/issues/new?labels=bug&template=bug-report---.md">Report Bug</a>
    ·
    <a href="https://github.com/ThePhoneProject/advanced-android-camera/issues/new?labels=enhancement&template=feature-request---.md">Request Feature</a>
  </p>
</div>



<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>



<!-- ABOUT THE PROJECT -->
## About The Project
We all know that camera quality is important when choosing and using a phone - 71% of people said so [in this poll](https://viewpoints.xyz/polls/small-phones). The sensor and SoC/ISP hardware is only part of the story. Incumbent manufacturers maintain their own highly advanced camera apps - this acts as a moat, making it harder for smaller companies to compete on the smartphone market, because they have to build an amazing camera app from scratch.

The lack of advanced open-source camera apps limits competition in the smartphone market, and makes it harder for new smartphone manufacturers to start up. **Commercial licenses** are available to manufacturers which can include customisations and hardware-integrations.

If we, the Phone Enthusiast Community, are able to build a great open-source camera app, it’ll open up the smartphone industry as a whole, and make niche phones more viable. 

<p align="right">(<a href="#readme-top">back to top</a>)</p>



### Built With

* [![Kotlin][kotlinlang.org]][Kotlin-url]
* [https://developer.android.com/jetpack](Jetpack Compose)
* [https://ai.google.dev/edge/litert](LiteRT) (previously known as Tensorflow Lite)
 <!-- TODO remove this (* [![Next][Next.js]][Next-url]) as soon as we get the Kotlin icon working -->

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started
This is an Android codebase, built using Google Android development tools and SDKs, therefore Google's documentation should cover many aspect of getting started with this project.

### Prerequisites

* Android Development environment: Many people use [Android Studio](https://developer.android.com/studio). More experienced and/or CLI developers will find ways that work for their operating system release and needs.

### Installation

1. Clone the repo
   ```sh
   git clone https://github.com/ThePhoneProject/advanced-android-camera.git
   cd advanced-android-camera
   ```
2. Build and install a debug release
   Connect an Android phone with developer mode enabled or configure an Android Virtual Device (AVD)
   ```sh
   gradlew installDebug
   ```
3. Start the camera app on the device
<!-- TODO add command line example to launch the default activity -->
   

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- USAGE EXAMPLES -->
## Usage

Experiment with the user interface and take photos and video recordings.

_For more examples, please refer to the [Documentation](https://example.com)_

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- ROADMAP -->
## Roadmap
### V0 Features ###
- [ ] Modes
    - [x] Photo   
        - [ ] Press and hold to record video
    - [x] Video
- [x] Auto Focus
    - [x] General
    - [x] with Touch to Focus Override
- [x] Zoom
    - [x] Preset buttons
    - [x] Slide between
    - [x] Pinch
    - [x] Auto-Switch between cameras
- [x] ⁠Auto Exposure
- [x] ⁠Front/Rear Camera Selection
- [x] ⁠Flash On/Off/Auto
- [x] [Added] Aspect Ratio selection
- [ ] [Added] QR Code Scanning
- [ ] [Added] Basic Settings Drawer


See the [open issues](https://github.com/ThePhoneProject/advanced-android-camera/issues) for a full list of proposed features (and known issues).

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**. Please use <https://gitmoji.dev/> in your commit messages, examples are available at <https://github.com/carloscuesta/gitmoji>.

If you have a suggestion that would make this better, please fork the repo and create a pull request using your github username as the first part of the name of your branch. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b your-github-username/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature and, as appropriate, tag the project issue raised for the work'`)
4. Push to the Branch (`git push origin your-github-username/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Top contributors:

<a href="https://github.com/ThePhoneProject/advanced-android-camera/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=ThePhoneProject/advanced-android-camera" alt="contrib.rocks image" />
</a>


<!-- LICENSE -->
## License

Distributed under the project_license. See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- CONTACT -->
## Contact

Joseph Reeve - [@isnit0](https://x.com/isnit0) 

Project Link: <https://github.com/ThePhoneProject/advanced-android-camera>

<p align="right">(<a href="#readme-top">back to top</a>)</p>


<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

* []()
* []()
* []()

<p align="right">(<a href="#readme-top">back to top</a>)</p>



<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/github/contributors/ThePhoneProject/advanced-android-camera.svg?style=for-the-badge
[contributors-url]: https://github.com/ThePhoneProject/advanced-android-camera/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/ThePhoneProject/advanced-android-camera.svg?style=for-the-badge
[forks-url]: https://github.com/ThePhoneProject/advanced-android-camera/network/members
[stars-shield]: https://img.shields.io/github/stars/ThePhoneProject/advanced-android-camera.svg?style=for-the-badge
[stars-url]: https://github.com/ThePhoneProject/advanced-android-camera/stargazers
[issues-shield]: https://img.shields.io/github/issues/ThePhoneProject/advanced-android-camera.svg?style=for-the-badge
[issues-url]: https://github.com/ThePhoneProject/advanced-android-camera/issues
[license-shield]: https://img.shields.io/github/license/gThePhoneProject/advanced-android-camera.svg?style=for-the-badge
[license-url]: https://github.com/ThePhoneProject/advanced-android-camera/blob/main/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://uk.linkedin.com/in/josephereeve
[product-screenshot]: images/screenshot.png
[Kotlin-url]: https://kotlinlang.org/
[Next-url]: https://nextjs.org/
