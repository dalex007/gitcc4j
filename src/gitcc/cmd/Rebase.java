package gitcc.cmd;

import gitcc.cc.CCCommit;
import gitcc.cc.CCFile;
import gitcc.cc.CCFile.Status;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class Rebase extends Command {

	@Override
	public void execute() throws Exception {
		cc.rebase();
		String branch = config.getBranch();
		boolean normal = branch != null;
		Date since = normal ? git.getCommitDate(config.getCC()) : null;
		Collection<CCCommit> commits = cc.getHistory(since);
		if (commits.isEmpty())
			return;
		// TODO Git fast import
		for (CCCommit c : commits) {
			handleFiles(c.getFiles());
			git.commit(c);
		}
		if (normal) {
			git.rebaseOnto(config.getCC(), config.getCI(), branch);
		}
		git.branch(config.getCC());
		git.tag(config.getCI(), "HEAD");
	}

	private void handleFiles(List<CCFile> files) {
		for (CCFile f : files) {
			if (f.getStatus() == Status.Directory) {
				handleFiles(cc.diffPred(f));
			} else if (f.getStatus() == Status.Added) {
				add(f);
			} else {
				remove(f);
			}
		}
	}

	private void add(CCFile f) {
		// TODO Full renames
		if (f.getVersion().getFullVersion().length() == 0) {
			System.out.println("IGNORED file: " + f);
			return;
		}
		File newFile = cc.get(f);
		File dest = new File(git.getRoot(), f.getFile());
		if (dest.exists() && !dest.delete())
			throw new RuntimeException("Could not delete file: " + dest);
		dest.getParentFile().mkdirs();
		if (!newFile.renameTo(dest)) {
			newFile.delete();
			throw new RuntimeException("Could not get " + dest);
		}
		git.add(f.getFile());
	}

	private void remove(CCFile f) {
		if (new File(git.getRoot(), f.getFile()).exists())
			git.remove(f.getFile());
	}
}
