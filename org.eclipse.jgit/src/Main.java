import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

/**
 * @author Marc Strapetz
 */
public class Main {

	// Static =================================================================

    public static void main(String[] args) throws IOException {
        final String mainRoot = "D:\\gittest\\worktree-main";
        final String wtRoot = "D:\\gittest\\worktree-branch";
        final Repository mainRepo = new FileRepositoryBuilder().setWorkTree(new File(mainRoot)).build();
        final Repository wtRepo = new FileRepositoryBuilder().setWorkTree(new File(wtRoot)).build();
        System.out.println(mainRepo.getCommonDirectory());
        System.out.println(mainRepo.readCommitEditMsg());
        mainRepo.writeCommitEditMsg("asd");

        for (Ref ref : mainRepo.getRefDatabase().getRefs()) {
            resolveRef(mainRepo, ref.getName());
        }
        for (Ref ref : wtRepo.getRefDatabase().getRefs()) {
            resolveRef(wtRepo, ref.getName());
        }
    }

	// Utils ==================================================================

    private static void resolveRef(Repository repo, String refName) throws IOException {
        final ObjectId resolve = repo.resolve(refName);
        System.out.println(repo.getWorkTree() + ": " + refName + " = " + (resolve != null ? resolve.getName() : "<null>"));
    }
}