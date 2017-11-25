/**
 *
 */
package version2.prototype.util;

import com.rits.cloning.Cloner;

import version2.prototype.Config;
import version2.prototype.ZonalSummary;
import version2.prototype.ProjectInfoMetaData.ProjectInfoFile;
import version2.prototype.ProjectInfoMetaData.ProjectInfoPlugin;
import version2.prototype.ProjectInfoMetaData.ProjectInfoSummary;
import version2.prototype.Scheduler.SchedulerStatus;

/**
 * @author Michael DeVos
 *
 */
public class EASTWebCloner {
    private static Cloner myClonerInstance = null;

    /**
     * Retrieves an initialized and pre-configured Cloner instance.
     * @return configured Cloner
     */
    public static Cloner GetClonerInstance()
    {
        if(myClonerInstance == null)
        {
            myClonerInstance = new Cloner();
            myClonerInstance.registerImmutable(ProjectInfoFile.class);
            myClonerInstance.registerImmutable(ProjectInfoPlugin.class);
            myClonerInstance.registerImmutable(ProjectInfoSummary.class);
            myClonerInstance.registerImmutable(SchedulerStatus.class);
            myClonerInstance.registerImmutable(EASTWebResult.class);
            myClonerInstance.registerImmutable(Config.class);
            myClonerInstance.registerImmutable(ZonalSummary.class);
        }
        return myClonerInstance;
    }

    private EASTWebCloner()
    {

    }

}
