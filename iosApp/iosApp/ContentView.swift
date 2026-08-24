import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let arguments = ProcessInfo.processInfo.arguments
        let screenshotScenarioName = arguments.firstIndex(of: "-screenshot-scenario").flatMap { index in
            let nextIndex = arguments.index(after: index)
            return nextIndex < arguments.endIndex ? arguments[nextIndex] : nil
        }

        return MainViewControllerKt.MainViewController(screenshotScenarioName: screenshotScenarioName)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .accessibilityIdentifier("RootComposeView")
            .ignoresSafeArea()
    }
}



