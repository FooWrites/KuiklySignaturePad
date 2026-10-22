import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        window = UIWindow(frame: UIScreen.main.bounds)
        // Default page is the 自研 DSL demo. Compose DSL: KUIKLY_PAGE=signature_pad_compose_demo
        let pageName = ProcessInfo.processInfo.environment["KUIKLY_PAGE"] ?? "signature_pad_demo"
        let vc = KuiklyRenderViewController(pageName: pageName, pageData: [:])
        let nav = UINavigationController(rootViewController: vc)
        nav.isNavigationBarHidden = true
        window?.rootViewController = nav
        window?.makeKeyAndVisible()
        return true
    }
}
