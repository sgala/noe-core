package noe.workspace

import groovy.util.logging.Slf4j
import noe.common.DefaultProperties
import noe.common.utils.Cleaner
import noe.common.utils.Library
import noe.common.utils.Version
import noe.eap.server.ServerEap
import noe.eap.server.as5.AS5Properties
import noe.eap.utils.Eap5Utils
import noe.server.AS5

@Slf4j
class WorkspaceAS5 extends WorkspaceAbstract {

  boolean secured = false
  Version eapVersion = DefaultProperties.eapVersion()

  WorkspaceAS5(boolean secured = false, String serverId = null) {
    super()
    this.secured = secured
    log.info('WorkspaceAS5(): BEGIN')

    if (secured) {
      this.basedir = Library.getUniversalProperty('as5.secured.basedir', getBasedir())
    } else {
      this.basedir = Library.getUniversalProperty('as5.unsecured.basedir', getBasedir())
    }

    def as5Profile
    if (secured) {
      as5Profile = Library.getUniversalProperty('as5.secured.profile', AS5Properties.PROFILE)
    } else {
      as5Profile = Library.getUniversalProperty('as5.unsecured.profile', AS5Properties.PROFILE)
    }
    def as5serverId = serverId ?: ServerEap.getPrefix()
    AS5 server = AS5.getInstance(basedir, '', context)
    server.setProfile(as5Profile)

    log.trace("HELL: servers map: ${serverController.servers}")
    log.trace("HELL: serverController.addServer(${as5serverId}, ${server.getBasedir()})")
    serverController.addServer(as5serverId, server)
    log.trace("HELL: servers map: ${serverController.servers}")
    initWorkspaceAS5()

    // initializing JMX user, it is needed for being able to properly shutdown the server
    server = serverController.getServerById(as5serverId)
    log.trace("HELL: serverController.getServerById(${as5serverId})=${server}")
    server.createManagementUser(AS5Properties.JMX_USER, AS5Properties.JMX_USER)
    log.info('WorkspaceAS5(): END')
  }

  def prepare() {
    log.info("Creating of new ${this.class.getName()} started")
    serverController.backup()
    log.info("Creating of new ${this.class.getName()} finished")
  }

  def destroy() {
    super.destroy()
    log.info("Destroying of default ${this.class.getName()} started")
    // TODO: Verify this
    if (!this.skipInstall) {
      if (deleteWorkspace) {
        Cleaner.cleanDirectoryBasedOnRegex(new File(getBasedir()), /.*(jboss-eap).*/)
        log.info("EAP ${this.class.getName()} deleted: ${basedir}")
      }
      log.info("EAP ${this.class.getName()} NOT deleted: ${basedir}")
    }
    log.info("Destroying of default ${this.class.getName()} finished")
  }

  /**
   * Install AS5
   * Static dir expected.
   */
  void installAS5() {
    log.debug("${this.class.getName()}.installAS5(): EAP BEGIN")
    def eap = new Eap5Utils(basedir, ant, platform, eapVersion.toString())
    eap.getIt(secured)
    log.debug("${this.class.getName()}.installAS5(): EAP END")
  }

  /**
   * Initialize the workspace.
   */
  void initWorkspaceAS5() {
    log.debug("${this.class.getName()}.initWorkspaceAS5(): BEGIN")
    if (!skipInstall) {
      installAS5()
    }
    log.debug("${this.class.getName()}.initWorkspaceAS5(): END")
  }

}
