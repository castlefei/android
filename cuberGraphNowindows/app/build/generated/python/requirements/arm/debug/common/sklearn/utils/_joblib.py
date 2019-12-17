import os as _os
import warnings as _warnings

# Chaquopy: workaround for https://github.com/joblib/joblib/issues/825. This will allow the
# joblib import to complete, but any scikit-learn features which use process-based parallelism
# will still not work. I'm not sure how widely-used these features are, so I'm not taking any
# further action on this unless a user reports a problem. If they do, we can advise them to use
# the `parallel_backend` context manager to use thread-based parallelism instead, and then
# we'll look into whether there's a way to monkey-patch joblib to do that by default.
import _multiprocessing
_multiprocessing.sem_unlink = None

with _warnings.catch_warnings():
    _warnings.simplefilter("ignore")
    # joblib imports may raise DeprecationWarning on certain Python
    # versions
    import joblib
    from joblib import logger
    from joblib import dump, load
    from joblib import __version__
    from joblib import effective_n_jobs
    from joblib import hash
    from joblib import cpu_count, Parallel, Memory, delayed
    from joblib import parallel_backend, register_parallel_backend


__all__ = ["parallel_backend", "register_parallel_backend", "cpu_count",
           "Parallel", "Memory", "delayed", "effective_n_jobs", "hash",
           "logger", "dump", "load", "joblib", "__version__"]
