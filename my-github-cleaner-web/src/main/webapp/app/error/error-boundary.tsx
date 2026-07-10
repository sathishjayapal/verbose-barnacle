import React from 'react';


export default class ErrorBoundary extends React.Component<IErrorBoundaryProps, IErrorBoundaryState> {

  override componentDidCatch(error: any, errorInfo: any) {
    this.setState({
      error,
      errorInfo,
    });
  }

  override render() {
    if (this.state?.error) {
      return (
          <div className="flex flex-col items-center justify-center min-h-[50vh] text-center">
            <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-8 md:p-12 max-w-lg w-full">
              <div className="mb-6">
                <svg className="w-20 h-20 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                     aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                        d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                </svg>
              </div>
              <h1 className="text-3xl font-bold text-gray-800 mb-2">Client error</h1>
              <p className="text-gray-500 mb-8">{this.state.error.toString()}</p>
              <button onClick={() => window.location.reload()}
                      className="inline-flex items-center justify-center px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                Reload page
              </button>
            </div>
          </div>
      );
    }
    return this.props.children;
  }

}

interface IErrorBoundaryProps {

  readonly children: React.ReactNode;

}

interface IErrorBoundaryState {

  readonly error: any;
  readonly errorInfo: any;

}
