gitPullRequestPipeline {
	dockerImageName = 'symbolplatform/build-ci:cpp-ubuntu-lts'
	prBranchName = 'fix/update_submodule'
	scriptSetupCommand = 'bash init.sh'
	scriptCommand = 'git submodule update --remote; git add .; git commit -m "[monorepo] fix: update submodule"'
	reviewers = []
}
